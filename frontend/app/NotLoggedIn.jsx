import React from 'react';
import PropTypes from 'prop-types';
import {Redirect} from 'react-router-dom';

class NotLoggedIn extends React.Component {
    static propTypes = {
        timeOut: PropTypes.number.isRequired,
        shouldTerminate: false
    };

    constructor(props){
        super(props);

        this.state = {
            timeRemaining: props.timeOut,
            timerId: null
        };

        this.tick = this.tick.bind(this);
    }

    tick() {
        this.setState({timeRemaining: this.state.timeRemaining-1});
    }

    componentWillUnmount(){
        if(this.state.timerId) window.clearInterval(this.state.timerId);
    }

    componentDidMount(){
        this.setState({timeRemaining: this.props.timeOut, timerId: window.setInterval(this.tick, 1000)});
    }

    render(){
        if(this.state.timeRemaining<1){
            return <Redirect to="/"/>;
        } else {
            return <div className="inline-dialog">
                <h2 className="inline-dialog-title">Not logged in</h2>
                <p className="inline-dialog-content centered">You are not currently logged in as anybody. Redirecting to
                    login page in {this.state.timeRemaining} seconds</p>
            </div>
        }
    }
}

export default NotLoggedIn;