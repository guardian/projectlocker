import React from 'react';
import PropTypes from 'prop-types';
import ErrorViewComponent from "../common/ErrorViewComponent.jsx";
import SummaryComponent from "./SummaryComponent.jsx";

class CommissionCompletionComponent extends React.Component {
    static propTypes = {
        workingGroupId: PropTypes.number,
        wgList: PropTypes.array,
        title: PropTypes.string
    };

    constructor(props) {
        super(props);

        this.state = {
            error: null,
            inProgress: false
        };
        this.confirmClicked = this.confirmClicked.bind(this);
    }

    confirmClicked(evt){

    }

    render() {
        return <div>
            <h3>Create new commission</h3>
            <p className="information">We will create a new commission with the information below.</p>
            <p className="information">Press "Confirm" to go ahead, or press Previous if you need to amend any details.</p>

            <ErrorViewComponent error={this.state.error}/>

            <SummaryComponent commissionName={this.props.title} wgList={this.props.wgList} selectedWorkingGroupId={this.props.workingGroupId}/>
            <span style={{float: "right"}}>
                <button onClick={this.confirmClicked}
                        disabled={this.state.inProgress}
                        style={{color: this.state.inProgress ? "lightgrey" : "black"}}
                >Confirm</button>
            </span>
        </div>
    }
}

export default CommissionCompletionComponent;