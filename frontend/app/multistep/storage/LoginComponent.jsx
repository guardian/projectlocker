import React from 'react';
import PropTypes from 'prop-types';

class StorageLoginComponent extends React.Component {
    static propTypes = {
        strgTypes: PropTypes.array.isRequired,
        valueWasSet: PropTypes.func.isRequired,
        loginDetails: PropTypes.object.isRequired
    };

    componentWillMount(){
        this.setState({
            hostname: this.props.loginDetails.hostname,
            port: this.props.loginDetails.port,
            device: this.props.loginDetails.device,
            username: this.props.loginDetails.username,
            password: this.props.loginDetails.password
        })
    }

    constructor(props){
        super(props);

        this.state = {
            hostname: "",
            port: "",
            device: "",
            username: "",
            password: ""
        }
    }

    componentDidUpdate(prevProps,prevState){
        if(prevState!==this.state) this.props.valueWasSet(this.state);
    }

    render() {
        const selectedStorage = this.props.strgTypes[this.props.selectedType];

        if(!selectedStorage.needsLogin){
            return(
                <div>
                    <h3>Storage Login</h3>
                    <p>{selectedStorage.name} storage does not require login.</p>
                </div>
            )
        } else {
            return (
                <div>
                    <h3>Storage Login</h3>
                    <p>{selectedStorage.name} is a remote storage.  Please supply the information we need to log in to it.  If any field
                        is not applicable, leave it blank
                    </p>
                    <form>
                        <table className="full-width">
                            <tbody>
                            <tr>
                                <td className="narrow">Storage host (or bucket name)</td>
                                <td><input className="full-width" id="hostname_input" value={this.state.hostname} onChange={(event)=>this.setState({hostname: event.target.value})}/></td>
                            </tr>
                            <tr>
                                <td className="narrow">Storage port (if applicable)</td>
                                <td><input className="full-width" id="port_input" value={this.state.port} onChange={(event)=>this.setState({port: event.target.value})}/></td>
                            </tr>
                            <tr>
                                <td className="narrow">Device (if applicable)<p className="explanation">For ObjectMatrix storage, specify the "cluster ID" and "vault ID" separated by a comma</p></td>
                                <td><input className="full-width" id="device_input" value={this.state.device} onChange={event=>this.setState({device: event.target.value})}/></td>
                            </tr>
                            <tr>
                                <td className="narrow">User name</td>
                                <td><input className="full-width" id="username_input" value={this.state.username} onChange={(event)=>this.setState({username: event.target.value})}/></td>
                            </tr>
                            <tr>
                                <td className="narrow">Password</td>
                                <td><input className="full-width" id="password_input" type="password" value={this.state.password} onChange={(event)=>this.setState({password: event.target.value})}/></td>
                            </tr>
                            </tbody>
                        </table>
                    </form>
                </div>)
        }
    }
}

export default StorageLoginComponent;
