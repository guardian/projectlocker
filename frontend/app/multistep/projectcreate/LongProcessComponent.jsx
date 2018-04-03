import React from 'react';
import PropTypes from 'prop-types';

class LongProcessComponent extends React.Component {
    static propTypes = {
        inProgress: PropTypes.bool.isRequired,
        expectedDuration: PropTypes.number.isRequired,
        operationName: PropTypes.string
    };

    constructor(props){
        super(props);

        this.state = {
            currentTime: 0,
            timerId: null
        }
    }

    componentDidUpdate(oldProps, oldState){
        if(oldProps.inProgress===false && this.props.inProgress===true){
            this.startTimer();
        } else if(oldProps.inProgress===true && this.props.inProgress===false){
            this.stopTimer();
        }
    }

    startTimer(){
        this.setState({timerId: window.setInterval(()=>this.setState({currentTime: this.state.currentTime+1}),1000)});
    }

    stopTimer(){
        if(this.state.timerId){
            window.clearInterval(this.state.timerId);
            this.setState({timerId: null});
        }
    }

    render(){
        return <div style={{display: this.props.inProgress ? "inline" : "none"}}>
            <img src="/assets/images/uploading.svg" style={{height: "20px", verticalAlign: "middle"}}/>
            <span style={{marginLeft: "1em"}}>
                {this.props.operationName} in progress. This may take {this.props.expectedDuration} seconds or more, please wait...<br/>
                You have been waiting for {this.state.currentTime} seconds so far.
            </span>
        </div>
    }
}

export default LongProcessComponent;