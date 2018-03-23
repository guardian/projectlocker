import React from 'react';
import PropTypes from 'prop-types';
import axios from 'axios';
import ErrorViewComponent from './multistep/common/ErrorViewComponent.jsx';
import {Link} from 'react-router-dom';

class LoginComponent extends React.Component {
    static propTypes = {
        onLoggedIn: PropTypes.func.isRequired,
        onLoggedOut: PropTypes.func.isRequired,
        username: PropTypes.string,
        currentlyLoggedIn: PropTypes.bool.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            loading: false,
            enteredUserName: "",
            enteredPassword: "",
            error: null,
            haveChecked: false
        };

        this.doLogout = this.doLogout.bind(this);
        this.doLogin = this.doLogin.bind(this);
    }

    doLogout() {
        this.setState({loading:true}, ()=>
            axios.post("/api/logout")
                .then(response=>{
                    this.setState({
                        loading: false,
                        enteredUserName: "",
                        enteredPassword: ""
                    }, ()=>{
                        this.props.onLoggedOut(response.data)
                    });
                })
                .catch(error=>{
                    this.setState({loading: false, error: error})
                })
        );
    }

    doLogin() {
        this.setState({loading: true}, ()=>
            axios.post("/api/login",{username: this.state.enteredUserName, password: this.state.enteredPassword})
                .then(response=>{
                    /* login successful. Update internal state and then call the parent */
                    /* the browser should have stored the PLAY_SESSION cookie for us */
                    this.setState({
                        loading: false,
                        enteredUserName: "",
                        enteredPassword: ""
                    }, ()=>{
                        this.props.onLoggedIn(response.data.uid, response.data.isAdmin)
                    });
                })
                .catch(error=>{
                    this.setState({loading: false, error: error})
                })
        );
    }

    render(){
        if(this.props.currentlyLoggedIn){
            return <div className="inline-dialog">
                <h2 className="inline-dialog-title">Login</h2>
                <p className="inline-dialog-content centered">You are currently logged in as
                    <i className="fa fa-user" style={{ marginRight: "3px", marginLeft: "5px"}}/>
                    <span className="emphasis">{this.props.username}</span></p>
                <p className="inline-dialog-content centered emphasis" style={{fontSize: "1.5em"}}><Link to="/project/?mine">Go to my projects ></Link></p>
                <p className="intro-banner">Or, please select an option on the left</p>
                <button className="inline-dialog" onClick={this.doLogout}>Log out</button>
            </div>
        } else {
            return <div className="inline-dialog">
                <h2 className="inline-dialog-title">Login</h2>
                <table className="login-table">
                    <tbody>
                    <tr>
                        <td>User name:</td>
                        <td>
                            <input id="username" type="text" className={this.state.loading ? "disabled" : ""} onChange={event=>this.setState({enteredUserName: event.target.value})}/>
                        </td>
                    </tr>
                    <tr>
                        <td>Password:</td>
                        <td>
                            <input id="password" type="password" className={this.state.loading ? "disabled" : ""} onChange={event=>this.setState({enteredPassword: event.target.value})}/>
                        </td>
                    </tr>
                    </tbody>
                </table>
                <span className="inline-dialog-content"><ErrorViewComponent error={this.state.error}/></span>
                <img src="/assets/images/uploading.svg" className="smallicon" style={{display: this.state.loading ? "block" : "none"}}/>


                <button className={this.state.loading ? "inline-dialog disabled" : "inline-dialog"} onClick={this.doLogin}>Log In</button>
            </div>
        }
    }
}

export default LoginComponent;
