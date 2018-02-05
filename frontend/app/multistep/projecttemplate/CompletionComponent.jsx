import React from 'react';
import PropTypes from 'prop-types';
import axios from 'axios';
//import SummaryComponent from './SummaryComponent.jsx';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';

class TemplateCompletionComponent extends React.Component {
    static propTypes ={
        currentEntry: PropTypes.number.isRequired,
        template: PropTypes.object.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            inProgress: false,
            error: null
        };
        this.confirmClicked = this.confirmClicked.bind(this);
    }

    confirmClicked(event){
        this.setState({inProgress: true});
        const restUrl = this.props.currentEntry ? "/api/template/" + this.props.currentEntry : "/api/template";

        axios.put(restUrl,this.requestContent()).then(
            (response)=>{
                this.setState({inProgress: false});
                window.location.assign('/template/');
            }
        ).catch(
            (error)=>{
                this.setState({inProgress: false, error: error});
                console.error(error)
            }
        )
    }

    requestContent(){
        /* returns an object of keys/values to send to the server for saving */
        return {

        }
    }

    render() {
        return(<div>
            <h3>Set up storage</h3>
            <p className="information">We will set up a new project template definition with the information below.</p>
            <p className="information">Press "Confirm" to go ahead, or press Previous if you need to amend any details.</p>
            <ErrorViewComponent error={this.state.error}/>
            <span style={{float: "right"}}><button onClick={this.confirmClicked}>Confirm</button></span>
        </div>)
    }
}

export default TemplateCompletionComponent;
