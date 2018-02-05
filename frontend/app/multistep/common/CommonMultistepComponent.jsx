import React from 'react';

/* common methods for all multistep windows*/
class CommonMultistepComponent extends React.Component {
    componentDidUpdate(prevProps,prevState){
        if(this.props.valueWasSet && prevState!=this.state) this.props.valueWasSet(this.state);
    }

}

export default CommonMultistepComponent;